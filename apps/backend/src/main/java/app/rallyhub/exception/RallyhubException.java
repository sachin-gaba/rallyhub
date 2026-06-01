package app.rallyhub.exception;

import lombok.Getter;

@Getter
public class RallyhubException extends RuntimeException {
    private final String code;
    private final int status;

    public RallyhubException(String code, String message, int status) {
        super(message);
        this.code   = code;
        this.status = status;
    }

    public static RallyhubException notFound(String entity) {
        return new RallyhubException("NOT_FOUND", entity + " not found", 404);
    }

    public static RallyhubException forbidden(String message) {
        return new RallyhubException("FORBIDDEN", message, 403);
    }

    public static RallyhubException conflict(String message) {
        return new RallyhubException("CONFLICT", message, 409);
    }

    public static RallyhubException badRequest(String message) {
        return new RallyhubException("VALIDATION", message, 400);
    }

    public static RallyhubException unauthorized() {
        return new RallyhubException("UNAUTHORIZED", "Authentication required", 401);
    }
}
