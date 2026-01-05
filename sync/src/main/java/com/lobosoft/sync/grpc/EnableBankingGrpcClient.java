package com.lobosoft.sync.grpc;

import com.lobosoft.enablebanking.grpc.EnableBankingGrpcServiceGrpc;
import lombok.Getter;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@Getter
public class EnableBankingGrpcClient {

    @GrpcClient("enablebanking")
    private EnableBankingGrpcServiceGrpc.EnableBankingGrpcServiceBlockingStub stub;
}
