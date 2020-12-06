package io.netty.example.common.keepalive;

import io.netty.example.common.OperationResult;
import lombok.Data;

@Data
public class KeepaliveOperationResult extends OperationResult {

    private final long time;

}
