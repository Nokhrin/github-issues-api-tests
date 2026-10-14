package org.nokhrin.github.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nokhrin.github.grpc.generated.GetIssueRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("grpc")
public class MetadataTest extends GrpcBaseTest {

    @Test
    void call_withoutAuthMetadata_returnsUnauthenticated() {
        StatusRuntimeException exception = assertThrows(
            StatusRuntimeException.class,
            () -> unAuthService.getIssue(
                GetIssueRequest.newBuilder()
                    .setNumber(1L)
                    .build()
            )
        );

        assertEquals(Status.Code.UNAUTHENTICATED, exception.getStatus().getCode());
    }


    @Test
    void call_withInvalidAuthMetadata_returnsUnauthenticated() {
        StatusRuntimeException exception = assertThrows(
            StatusRuntimeException.class,
            () -> invalidAuthService.getIssue(
                GetIssueRequest.newBuilder()
                    .setNumber(1L)
                    .build()
            )
        );

        assertEquals(Status.Code.UNAUTHENTICATED, exception.getStatus().getCode());
    }
}
