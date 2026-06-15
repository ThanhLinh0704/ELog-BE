package com.elog.security;

import com.elog.entity.User;
import com.elog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

                // Duyệt qua tập hợp roles và map thành danh sách GrantedAuthority
                List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                                .toList();

                return org.springframework.security.core.userdetails.User
                                .withUsername(user.getUsername())
                                .password(user.getPasswordHash())
                                .authorities(authorities)
                                .build();
        }

}
