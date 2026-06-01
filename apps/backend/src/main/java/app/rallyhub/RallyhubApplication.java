package app.rallyhub;

import io.micronaut.runtime.Micronaut;
import io.micronaut.core.annotation.Introspected;

@Introspected
public class RallyhubApplication {
    public static void main(String[] args) {
        Micronaut.run(RallyhubApplication.class, args);
    }
}
