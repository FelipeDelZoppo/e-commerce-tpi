package tpi.backend.e_commerce.services.JwtService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;
import tpi.backend.e_commerce.dto.auth.request.SignInRequest;
import tpi.backend.e_commerce.dto.auth.request.SignUpRequest;
import tpi.backend.e_commerce.mapper.UserMapper;
import tpi.backend.e_commerce.models.User;
import tpi.backend.e_commerce.repositories.IUserRepository;
import tpi.backend.e_commerce.services.JwtService.interfaces.IAuthenticationService;
import tpi.backend.e_commerce.validation.Validation;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    private final Validation validation;

    @Override
    public ResponseEntity<?> signup(SignUpRequest request, BindingResult result) {

        // Si hay algun error de validacion, retornara un 400 con los errores
        if (result.hasFieldErrors()) {
            return validation.validate(result);
        }

        // Chequea que el email no exista en la BD
        if (userRepository.existsByEmail(request.getEmail())) {
            return validation.validate(
                    "email",
                    "Ya existe un usuario con ese email",
                    409);
        }

        //Si se quiere registrar a un ADMIN, se valida que el JWT enviado sea de un ADMIN
        if (request.isAdmin()) {
            try {
                validation.validateRole(request.getJwt());
            } catch (Exception e) {
                return validation.validate(
                  "jwt",
                  "Solo los usuarios ADMIN pueden registrar otros ADMIN",
                  403  
                );
            }
        }

        // Si no hay errores, guarda al usuario en la BD y retorna el JWT
        User user = UserMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        String jwt = jwtService.generateToken(user, user.getRole());
        return ResponseEntity.ok(UserMapper.toJwtDto(user, jwt));
    }

    @Override
    public ResponseEntity<?> signin(SignInRequest request, BindingResult result) {

        if (result.hasFieldErrors()) {
            return validation.validate(result);
        }

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            return validation.validate(
                    "email",
                    "No existe un usuario con ese email",
                    404);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

        } catch (AuthenticationException e) {
            return validation.validate(
                    "password",
                    "La contraseña es incorrecta",
                    401);
        }
        User user = optionalUser.get();
        String jwt = jwtService.generateToken(user, user.getRole());
        return ResponseEntity.ok(UserMapper.toJwtDto(user, jwt));
    }
}

