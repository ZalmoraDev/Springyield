package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.AuthSessionDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.AuthRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.stefvisser.springyield.dto.UserProfileDto;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

@Primary
@Service
public class AuthServiceImpl implements AuthService {
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public AuthSessionDto signup(UserSignupDto dto) {
        if (dto == null || dto.getEmail() == null || dto.getPassword() == null)
            // If required fields are missing, throw a bad request exception
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all required fields are filled in");

        if (authRepository.existsByEmail(dto.getEmail())) {
            // If the email is already registered, throw a conflict exception
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered");
        }

        User user = new User(
                dto.getFirstName(),
                dto.getLastName(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getEmail(),
                dto.getBsnNumber(),
                dto.getPhoneNumber(),
                UserRole.UNAPPROVED,
                new ArrayList<>()
        );

        // Add user to DB
        authRepository.save(user);
        return createNewLoginSession(user);
    }

    public AuthSessionDto login(UserLoginDto loginDto) {
        // Login user after successful signup
        User user = authRepository.findByEmail(loginDto.getEmail());

        if (user == null || !passwordEncoder.matches(loginDto.getPassword(), user.getPassword()))
            // If the user does not exist, throw an unauthorized exception
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");

        return createNewLoginSession(user);
    }

    // Create a new login session for the user, used by signup and login
    private AuthSessionDto createNewLoginSession(User user) {
        UserProfileDto userProfile = new UserProfileDto(user);
        String token = this.generateJwtToken(userProfile);
        return new AuthSessionDto(token, userProfile);
    }

    //------------------------------------------------------------------------------------------------------------------

    public String generateJwtToken(UserProfileDto user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // valid for 15m, upon page transition is checked if the token is still valid
                .signWith(key)
                .compact();
    }

    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
