package com.lobosoft.api.auth;

import com.lobosoft.auth.grpc.AuthGrpcServiceGrpc;
import com.lobosoft.auth.grpc.ValidateTokenRequest;
import com.lobosoft.auth.grpc.ValidateTokenResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }

            return res.getUserId();
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();

            if (code == Status.Code.UNAUTHENTICATED) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }

            if (code == Status.Code.UNAVAILABLE) {
                log.error("Auth service unavailable: {}", e.getStatus(), e);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth service unavailable", e);
            }

            log.error("Error talking to auth service: {}", e.getStatus(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Auth service error: " + code, e);
        } catch (Exception e) {
            log.error("Unexpected error in AuthGrpcClient", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Auth error", e);
        }
    }
}
