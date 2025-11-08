package com.example.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * AuthBeansConfig: تُرك فارغًا بعد نقل/تثبيت مزوّد المصادقة داخل SecurityConfig.
 * لا تُعرّفي هنا أي @Bean من نوع AuthenticationProvider.
 */
@Configuration
@RequiredArgsConstructor
public class AuthBeansConfig {
    // لا Beans هنا من نوع AuthenticationProvider
}

//package com.example.security.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//public class AuthBeansConfig {
//    @Bean
//    public AuthenticationProvider authenticationProvider(UserDetailsService uds,
//                                                         PasswordEncoder encoder) {
//        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
//        p.setUserDetailsService(uds);
//        p.setPasswordEncoder(encoder);
//        return p;
//    }
//}
