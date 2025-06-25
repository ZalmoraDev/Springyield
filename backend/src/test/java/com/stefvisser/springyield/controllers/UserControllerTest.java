package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.UserApprovalDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserUpdateDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserProfileDto testUserProfileDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup a test user
        testUser = new User(
                "John",
                "Doe",
                "password",
                "john.doe@example.com",
                123456789,
                "123-456-7890",
                UserRole.APPROVED,
                new ArrayList<>()
        );
        testUser.setUserId(1L);

        // Setup a test user profile DTO
        testUserProfileDto = new UserProfileDto(testUser);
    }

    @Test
    void getUserById_Success() {
        // Arrange
        Long userId = 1L;
        when(userService.getUserById(any(User.class), eq(userId))).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = userController.getUserById(testUser, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof UserProfileDto);
        UserProfileDto returnedDto = (UserProfileDto) response.getBody();
        assertEquals(testUser.getUserId(), returnedDto.getUserId());
    }

    @Test
    void getUserById_NotFound() {
        // Arrange
        Long userId = 999L;
        when(userService.getUserById(any(User.class), eq(userId)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Act
        ResponseEntity<?> response = userController.getUserById(testUser, userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }

    @Test
    void search_Success() {
        // Arrange
        String query = "John";
        UserRole role = UserRole.APPROVED;
        int limit = 10;
        int offset = 0;

        List<UserProfileDto> userList = new ArrayList<>();
        userList.add(testUserProfileDto);
        PaginatedDataDto<UserProfileDto> paginatedData = new PaginatedDataDto<>(userList, 1);

        when(userService.search(any(User.class), eq(query), eq(role), eq(limit), eq(offset)))
                .thenReturn(paginatedData);

        // Act
        ResponseEntity<?> response = userController.search(testUser, query, role, limit, offset);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof PaginatedDataDto);
        PaginatedDataDto<UserProfileDto> returnedData = (PaginatedDataDto<UserProfileDto>) response.getBody();
        assertEquals(1, returnedData.getTotalCount());
        assertEquals(1, returnedData.getData().size());
    }

    @Test
    void approveUser_Success() {
        // Arrange
        Long userId = 1L;
        UserApprovalDto approvalDto = new UserApprovalDto();
        approvalDto.setDailyLimit(new BigDecimal("1000.00"));
        approvalDto.setAbsoluteLimit(new BigDecimal("5000.00"));

        doNothing().when(userService).approveUser(any(User.class), eq(userId),
                any(BigDecimal.class), any(BigDecimal.class));

        // Act
        ResponseEntity<?> response = userController.approveUser(testUser, userId, approvalDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User approved successfully.", response.getBody());
        verify(userService, times(1)).approveUser(any(User.class), eq(userId),
                eq(approvalDto.getDailyLimit()), eq(approvalDto.getAbsoluteLimit()));
    }

    @Test
    void updateUser_Success() {
        // Arrange
        Long userId = 1L;
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("Jane");
        updateDto.setLastName("Smith");

        when(userService.updateUser(any(User.class), eq(userId), any(UserUpdateDto.class)))
                .thenReturn(testUserProfileDto);

        // Act
        ResponseEntity<?> response = userController.updateUser(testUser, userId, updateDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User updated successfully.", response.getBody());
        verify(userService, times(1)).updateUser(any(User.class), eq(userId), eq(updateDto));
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        Long userId = 1L;
        doNothing().when(userService).deleteUser(any(User.class), eq(userId));

        // Act
        ResponseEntity<?> response = userController.deleteUser(testUser, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully.", response.getBody());
        verify(userService, times(1)).deleteUser(any(User.class), eq(userId));
    }

    @Test
    void deleteUser_Forbidden() {
        // Arrange
        Long userId = 1L;
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed"))
                .when(userService).deleteUser(any(User.class), eq(userId));

        // Act
        ResponseEntity<?> response = userController.deleteUser(testUser, userId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Not allowed", response.getBody());
    }
}
