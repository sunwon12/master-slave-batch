package com.example.demo.service;

import com.example.demo.dto.UserCreateRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.DuplicateEmailException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 생성 - 쓰기 작업 (Master DB 사용)
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        log.info("사용자 생성 중: 이메일={}", request.getEmail());

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .build();

        User savedUser = userRepository.save(user);
        log.info("사용자 생성 완료: id={}", savedUser.getId());
        return savedUser;
    }

    /**
     * 사용자 조회 by ID - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.info("사용자 조회 중: id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * 사용자 조회 by Email - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.info("사용자 조회 중: 이메일={}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
