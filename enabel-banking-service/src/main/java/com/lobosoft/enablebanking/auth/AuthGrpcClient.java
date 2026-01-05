package com.lobosoft.enablebanking.auth;

import com.lobosoft.auth.grpc.AuthGrpcServiceGrpc;
import com.lobosoft.auth.grpc.ValidateTokenRequest;
import com.lobosoft.auth.grpc.ValidateTokenResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthGrpcClient {

    @GrpcClient("authService")
    private AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub stub;

    public String validateTokenAndGetUserId(String token) {
        log.debug("AuthGrpcClient.validateTokenAndGetUserId called");
        try {
            ValidateTokenRequest req = ValidateTokenRequest.newBuilder()
                    .setAccessToken(token)
                    .build();

            log.debug("Sending gRPC validateToken request");
            ValidateTokenResponse res = stub.validateToken(req);
            log.debug("Received grpc response: valid={} userId={}",
                    res.getValid(), res.getUserId());

            if (!res.getValid()) {
                log.warn("Token invalid according to auth service");
                throw new IllegalArgumentException("Invalid token");
            }

            return res.getUserId();
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();

            if (code == Status.Code.UNAUTHENTICATED) {
                log.warn("Token rejected by auth service: {}", e.getStatus());
                throw new InvalidTokenException("Invalid token", e);
            } else {
                log.error("Error talking to auth service: {}", e.getStatus());
                throw new AuthServiceUnavailableException("Auth service error: " + code);
            }
        } catch (Exception e) {
            log.error("Unexpected error in AuthGrpcClient", e);
            throw e;
        }
    }
}
