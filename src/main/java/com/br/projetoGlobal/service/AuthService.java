package com.br.projetoGlobal.service;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.br.projetoGlobal.config.security.jwt.JwtUtils;
import com.br.projetoGlobal.controllers.payload.dtos.requestDTO.LoginRequestDTO;
import com.br.projetoGlobal.controllers.payload.dtos.requestDTO.SignupRequestDTO;
import com.br.projetoGlobal.controllers.payload.dtos.responseDTO.JwtResponseDTO;
import com.br.projetoGlobal.controllers.payload.dtos.responseDTO.MessageResponseDTO;
import com.br.projetoGlobal.models.EmpresaMock;
import com.br.projetoGlobal.models.Role;
import com.br.projetoGlobal.models.Usuario;
import com.br.projetoGlobal.models.Enums.RoleEnum;
import com.br.projetoGlobal.repository.EmpresaMockRepository;
import com.br.projetoGlobal.repository.RoleRepository;
import com.br.projetoGlobal.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    EmpresaMockRepository empresaMockRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    public ResponseEntity<?> authenticateUser(LoginRequestDTO loginRequestDTO) throws Exception {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            return ResponseEntity.ok(new JwtResponseDTO(jwt));
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> registerUser(SignupRequestDTO signUpRequestDTO) throws Exception {
        if (usuarioRepository.existsByEmail(signUpRequestDTO.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponseDTO("Não é possível cadastrar esse usuário."));
        }

        Usuario usuario = new Usuario(signUpRequestDTO.getEmail(),
                signUpRequestDTO.getEmail(),
                encoder.encode(signUpRequestDTO.getPassword()));

        Set<String> strRoles = signUpRequestDTO.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
                        roles.add(adminRole);

                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(RoleEnum.ROLE_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
                        roles.add(modRole);

                        break;
                    default:
                        Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Erro: Role não encontrada"));
                        roles.add(userRole);
                }
            });
        }

        usuario.setRoles(roles);
        usuario.setName(signUpRequestDTO.getName());

        Usuario savedUser = this.usuarioRepository.save(usuario);

        Random random = new Random();
        this.empresaMockRepository.save(new EmpresaMock(savedUser, random.nextLong(100001 - 1000) + 1000));

        return authenticateUser(new LoginRequestDTO(signUpRequestDTO.getEmail(), signUpRequestDTO.getPassword()));
    }
}
