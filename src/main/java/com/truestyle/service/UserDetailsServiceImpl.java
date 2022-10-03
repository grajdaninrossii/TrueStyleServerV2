package com.truestyle.service;

import com.truestyle.entity.user.Gender;
import com.truestyle.entity.user.User;
import com.truestyle.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

//    public UserDetailsServiceImpl(UserRepository userRepository) {this.userRepository = userRepository;}

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username:" + username));
        return UserDetailsImpl.build(user);
    }

    public Boolean setNumberOfUser(String number){
        return false;
    }

    public Boolean setGenderOfUser(Gender gender){
        return false;
    }

    public Boolean setCountryOfUser(String country){
        return false;
    }

    public Boolean setPhotoOfUser(String photo_url){
        return false;
    }
}
