package com.example.demo.controller;

import com.example.demo.dto.UserCreateRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 관리 API - 쓰기 작업은 Master DB, 읽기 작업은 Slave DB 사용")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "사용자 등록",
            description = "새로운 사용자를 등록합니다. (쓰기 작업 - Master DB)"
    )
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    @Operation(
            summary = "사용자 ID로 조회",
            description = "사용자 ID로 사용자 정보를 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id
    ) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Operation(
            summary = "이메일로 사용자 조회",
            description = "이메일로 사용자 정보를 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(
            @Parameter(description = "사용자 이메일", required = true, example = "user@example.com")
            @PathVariable String email
    ) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
