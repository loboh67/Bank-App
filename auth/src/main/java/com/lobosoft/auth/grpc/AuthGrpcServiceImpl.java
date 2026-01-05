// grpc/AuthGrpcServiceImpl.java
package com.lobosoft.auth.grpc;

import com.lobosoft.auth.domain.UserRepository;
import com.lobosoft.auth.security.JwtService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class AuthGrpcServiceImpl extends AuthGrpcServiceGrpc.AuthGrpcServiceImplBase {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void validateToken(ValidateTokenRequest request,
                              StreamObserver<ValidateTokenResponse> responseObserver) {
        String token = request.getAccessToken();
        try {
            UUID userId = jwtService.parseUserId(token);

            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                var resp = ValidateTokenResponse.newBuilder()
                        .setValid(false)
                        .build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
                return;
            }

            var user = userOpt.get();

            var resp = ValidateTokenResponse.newBuilder()
                    .setValid(true)
                    .setUserId(user.getId().toString())
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("invalid_token")
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
