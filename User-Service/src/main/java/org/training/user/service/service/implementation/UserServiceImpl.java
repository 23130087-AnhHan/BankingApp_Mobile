package org.training.user.service.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.training.user.service.exception.EmptyFields;
import org.training.user.service.exception.EmailSendingException;
import org.training.user.service.exception.ResourceConflictException;
import org.training.user.service.exception.ResourceNotFound;
import org.training.user.service.external.AccountService;
import org.training.user.service.model.Status;
import org.training.user.service.model.dto.CreateUser;
import org.training.user.service.model.dto.UserDto;
import org.training.user.service.model.dto.UserUpdate;
import org.training.user.service.model.dto.UserUpdateStatus;
import org.training.user.service.model.dto.response.Response;
import org.training.user.service.model.entity.User;
import org.training.user.service.model.entity.UserProfile;
import org.training.user.service.model.external.Account;
import org.training.user.service.model.mapper.UserMapper;
import org.training.user.service.repository.UserRepository;
import org.training.user.service.service.KeycloakService;
import org.training.user.service.service.EmailService;
import org.training.user.service.service.OtpService;
import org.training.user.service.service.UserService;
import org.training.user.service.utils.FieldChecker;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final AccountService accountService;
    private final OtpService otpService;
    private final EmailService emailService;

    private UserMapper userMapper = new UserMapper();

    @Value("${spring.application.success}")
    private String responseCodeSuccess;

    @Value("${spring.application.not_found}")
    private String responseCodeNotFound;

    /**
     * Creates a new user.
     *
     * @param userDto The user data transfer object containing user information.
     * @return A response indicating the result of the user creation.
     * @throws ResourceConflictException If the emailId is already registered as a user.
     * @throws RuntimeException If the user with identification number is not found.
     */
    @Override
    public Response createUser(CreateUser userDto) {

        userDto.setEmailId(userDto.getEmailId().trim().toLowerCase(Locale.ROOT));

        if (userRepository.existsByEmailIdIgnoreCase(userDto.getEmailId())) {
            throw new ResourceConflictException("Email đã được đăng ký trước đó");
        }
        if (userRepository.existsByContactNo(userDto.getContactNumber())) {
            throw new ResourceConflictException("Số điện thoại đã được đăng ký trước đó");
        }

        List<UserRepresentation> userRepresentations = keycloakService.readUserByEmail(userDto.getEmailId());

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userDto.getEmailId());
        userRepresentation.setFirstName(userDto.getFirstName());
        userRepresentation.setLastName(userDto.getLastName());
        userRepresentation.setEmailVerified(false);
        // Keep the Keycloak account enabled so credentials can be checked at login;
        // the local emailVerified flag blocks token delivery until OTP verification.
        userRepresentation.setEnabled(true);
        userRepresentation.setEmail(userDto.getEmailId());

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setValue(userDto.getPassword());
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));

        String authId;
        if (userRepresentations.isEmpty()) {
            authId = keycloakService.createUser(userRepresentation);
        } else {
            // A previous DB transaction may have rolled back after Keycloak creation.
            // Reuse that orphan safely so the customer is not permanently locked out.
            UserRepresentation orphan = userRepresentations.get(0);
            authId = orphan.getId();
            orphan.setUsername(userDto.getEmailId());
            orphan.setEmail(userDto.getEmailId());
            orphan.setFirstName(userDto.getFirstName());
            orphan.setLastName(userDto.getLastName());
            orphan.setEnabled(true);
            orphan.setEmailVerified(false);
            keycloakService.updateUser(orphan);
            keycloakService.resetPassword(authId, credentialRepresentation);
            log.warn("Recovered orphan Keycloak user for email {}", userDto.getEmailId());
        }

            UserProfile userProfile = UserProfile.builder()
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName()).build();

            User user = User.builder()
                    .emailId(userDto.getEmailId())
                    .contactNo(userDto.getContactNumber())
                    .emailVerified(false)
                    .emailOtp(otpService.generateOtp())
                    .emailOtpExpiredAt(otpService.generateExpiredTime())
                    .status(Status.PENDING).userProfile(userProfile)
                    .authId(authId)
                    .identificationNumber(UUID.randomUUID().toString()).build();

            User savedUser = userRepository.saveAndFlush(user);
            String responseMessage = "Đăng ký thành công. Vui lòng kiểm tra email để xác thực OTP.";
            String responseCode = responseCodeSuccess;
            try {
                emailService.sendOtp(savedUser.getEmailId(), savedUser.getEmailOtp());
            } catch (EmailSendingException exception) {
                responseMessage = "Đăng ký thành công nhưng không thể gửi email OTP. Vui lòng thử gửi lại OTP.";
                responseCode = "502";
            }
            return Response.builder()
                    .responseMessage(responseMessage)
                    .responseCode(responseCode)
                    .userId(savedUser.getUserId())
                    .emailId(savedUser.getEmailId())
                    .displayName(userProfile.getFirstName() + " " + userProfile.getLastName())
                    .status(savedUser.getStatus())
                    .build();
    }

    /**
     * Retrieves all users and their corresponding details.
     *
     * @return a list of UserDto objects containing the user information
     */
    @Override
    public List<UserDto> readAllUsers() {

        List<User> users = userRepository.findAll();

        Map<String, UserRepresentation> userRepresentationMap = keycloakService.readUsers(users.stream().map(user -> user.getAuthId()).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(UserRepresentation::getId, Function.identity()));

        return users.stream().map(user -> {
            UserDto userDto = userMapper.convertToDto(user);
            UserRepresentation userRepresentation = userRepresentationMap.get(user.getAuthId());
            userDto.setUserId(user.getUserId());
            userDto.setEmailId(userRepresentation.getEmail());
            userDto.setIdentificationNumber(user.getIdentificationNumber());
            return userDto;
        }).collect(Collectors.toList());
    }

    /**
     * Reads a user from the database using the provided authId.
     *
     * @param authId the authentication id of the user
     * @return the UserDto object representing the user
     * @throws ResourceNotFound if the user is not found on the server
     */
    @Override
    public UserDto readUser(String authId) {

        User user = userRepository.findUserByAuthId(authId).
                orElseThrow(() -> new ResourceNotFound("User not found on the server"));

        UserRepresentation userRepresentation = keycloakService.readUser(authId);
        UserDto userDto = userMapper.convertToDto(user);
        userDto.setEmailId(userRepresentation.getEmail());
        return userDto;
    }

    /**
     * Updates the status of a user.
     *
     * @param id The ID of the user.
     * @param userUpdate The updated user status.
     * @return The response indicating the success of the update.
     * @throws ResourceNotFound If the user is not found.
     * @throws EmptyFields If the user has empty fields.
     */
    @Override
    public Response updateUserStatus(Long id, UserUpdateStatus userUpdate) {

        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFound("User not found on the server"));

        if (FieldChecker.hasEmptyFields(user)) {
            log.error("User is not updated completely");
            throw new EmptyFields("please updated the user", responseCodeNotFound);
        }

        if(userUpdate.getStatus().equals(Status.APPROVED)){
            UserRepresentation userRepresentation = keycloakService.readUser(user.getAuthId());
            userRepresentation.setEnabled(true);
            userRepresentation.setEmailVerified(true);
            keycloakService.updateUser(userRepresentation);
        }

        user.setStatus(userUpdate.getStatus());
        userRepository.save(user);

        return Response.builder()
                .responseMessage("User updated successfully")
                .responseCode(responseCodeSuccess).build();
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the ID of the user to retrieve
     * @return the UserDto object representing the user
     * @throws ResourceNotFound if the user is not found
     */
    @Override
    public UserDto readUserById(Long userId) {

        return userRepository.findById(userId)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFound("User not found on the server"));
    }

    /**
     * Updates a user with the given ID.
     *
     * @param id The ID of the user to update.
     * @param userUpdate The updated information for the user.
     * @return The response indicating the success or failure of the update operation.
     * @throws ResourceNotFound if the user with the given ID is not found.
     */
    @Override
    public Response updateUser(Long id, UserUpdate userUpdate) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found on the server"));

        user.setContactNo(userUpdate.getContactNo());
        BeanUtils.copyProperties(userUpdate, user.getUserProfile());
        userRepository.save(user);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("user updated successfully").build();
    }

    /**
     * Retrieves a UserDto by the given accountId.
     *
     * @param accountId The account ID of the user.
     * @return The UserDto object corresponding to the given accountId.
     * @throws ResourceNotFound If the account or user is not found on the server.
     */
    @Override
    public UserDto readUserByAccountId(String accountId) {

        ResponseEntity<Account> response = accountService.readByAccountNumber(accountId);
        if(Objects.isNull(response.getBody())){
            throw new ResourceNotFound("account not found on the server");
        }
        Long userId = response.getBody().getUserId();
        return userRepository.findById(userId)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFound("User not found on the server"));
    }
}
