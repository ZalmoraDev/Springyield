package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.UserApprovalDTO;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserUpdateDto;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.services.AuthService;
import com.stefvisser.springyield.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        authService = mock(AuthService.class);
        userController = new UserController(userService, authService);

        sampleUser = new User(
                "John", "Doe", "password", "john@example.com",
                123456789, "0612345678", UserRole.ADMIN, Collections.emptyList());
        sampleUser.setUserId(1L);
    }

    @Test
    void getUserByName_withValidAdminUser_returnsPaginatedResults() {
        PaginatedDataDTO<UserProfileDto> dummyResult = new PaginatedDataDTO<>(new ArrayList<>(), 0);
        when(userService.getUserById(1L)).thenReturn(sampleUser);
        when(userService.search("", null, 10, 0, true)).thenReturn(dummyResult);

        ResponseEntity<?> response = userController.getUserByName(sampleUser, null, null, null, 0);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dummyResult, response.getBody());
    }

    @Test
    void getUserByName_whenUserNotAuthenticated_returnsUnauthorized() {
        ResponseEntity<?> response = userController.getUserByName(null, null, null, null, 0);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void approveUser_withAuthorizedEmployee_returnsApprovedUserProfile() {
        UserApprovalDTO approvalDTO = new UserApprovalDTO(BigDecimal.valueOf(2000), BigDecimal.valueOf(5000));
        when(userService.getUserById(1L)).thenReturn(sampleUser);

        UserProfileDto approvedProfile = new UserProfileDto(sampleUser);
        when(userService.approveUser(2L, approvalDTO.getDailyLimit(), approvalDTO.getAbsoluteLimit()))
                .thenReturn(approvedProfile);

        ResponseEntity<?> response = userController.approveUser(sampleUser, 2L, approvalDTO);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(approvedProfile, response.getBody());
    }

    @Test
    void updateUser_whenUnauthorizedUserTriesToUpdateOtherUser_returnsForbidden() {
        User anotherUser = new User("Jane", "Smith", "pwd", "jane@example.com", 987654321, "0698765432", UserRole.APPROVED, Collections.emptyList());
        anotherUser.setUserId(2L);
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("new@example.com");

        when(userService.getUserById(1L)).thenReturn(sampleUser);
        sampleUser.setRole(UserRole.APPROVED); // not admin

        ResponseEntity<?> response = userController.updateUser(sampleUser, 2L, dto);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void getUserById_withSameUser_returnsProfile() {
        when(userService.getUserById(1L)).thenReturn(sampleUser);

        ResponseEntity<?> response = userController.getUserById(sampleUser, 1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof UserProfileDto);
    }

//    @Test
//    void deleteUser_withValidRequest_deletesUserSuccessfully() {
//        when(userService.getUserById(1L)).thenReturn(sampleUser);
//
//        ResponseEntity<?> response = userController.deleteUser(sampleUser, 1L);
//
//        assertEquals(200, response.getStatusCodeValue());
//        verify(userService).deleteUser(1L);
//    }

    @Test
    void deleteUser_whenUserNotAuthorized_returnsForbidden() {
        User regularUser = new User("Test", "User", "pwd", "test@user.com", 123456789, "0600000000", UserRole.UNAPPROVED, Collections.emptyList());
        regularUser.setUserId(2L);
        when(userService.getUserById(2L)).thenReturn(regularUser);

        ResponseEntity<?> response = userController.deleteUser(regularUser, 1L); // trying to delete someone else

        assertEquals(403, response.getStatusCodeValue());
    }
}
