package com.elog.security;

import com.elog.entity.User;
import com.elog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads user from DB for Spring Security.
 * Role mapped to GrantedAuthority: "ROLE_{name}" convention.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                User user = userRepository.findByUsernameAndIsActiveTrue(username)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found or inactive: " + username));

                // Duyệt qua tập hợp roles để lấy vai trò và các quyền tương ứng
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                user.getRoles().forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                        if (role.getPermissions() != null) {
                                role.getPermissions().forEach(permission -> {
                                        authorities.add(new SimpleGrantedAuthority(permission.getName()));
                                });
                        }
                });

                return org.springframework.security.core.userdetails.User
                                .withUsername(user.getUsername())
                                .password(user.getPasswordHash())
                                .authorities(authorities)
                                .build();
        }

}
