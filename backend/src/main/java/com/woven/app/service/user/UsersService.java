package com.woven.app.service.user;


import com.woven.app.domain.User;
import com.woven.app.repository.UserRepository;
import com.woven.app.web.dto.admin.UserCreateDto;
import com.woven.app.web.dto.admin.UserDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsersService implements UserDetailsService {

    private final UserRepository userRepository;

    public final PasswordEncoder passwordEncoder;

    public UsersService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Username Not Found"));
        return new AppUserDetails(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public UserDto create(UserCreateDto dto) {
        User entity = new User();
        entity.setUsername(dto.username());
        entity.setEmail(dto.email());
        entity.setPassword(passwordEncoder.encode(dto.password()));
        entity.setFullName(dto.fullName());
        entity.setRoles(dto.role());

        User savedEntity = userRepository.save(entity);
        return toDto(savedEntity);
    }

    @Transactional
    public UserDto update(Integer id, UserCreateDto dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Username Not Found"));

        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setFullName(dto.fullName());
        user.setRoles(dto.role());
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        return toDto(user);
    }

    @Transactional
    public void delete(Integer id) {
        userRepository.deleteById(id);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles()
        );
    }
}
