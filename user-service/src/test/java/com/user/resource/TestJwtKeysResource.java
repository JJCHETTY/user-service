package com.user.resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class TestJwtKeysResource implements QuarkusTestResourceLifecycleManager {
    private Path privateKeyPath;
    private Path publicKeyPath;

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            Path directory = Path.of("target", "test-jwt-keys").toAbsolutePath();
            Files.createDirectories(directory);
            privateKeyPath = directory.resolve("private.pem");
            publicKeyPath = directory.resolve("public.pem");
            Files.writeString(privateKeyPath, pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
            Files.writeString(publicKeyPath, pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));

            return Map.of();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create test JWT keys", exception);
        }
    }

    @Override
    public void stop() {
        try {
            if (privateKeyPath != null) {
                Files.deleteIfExists(privateKeyPath);
            }
            if (publicKeyPath != null) {
                Files.deleteIfExists(publicKeyPath);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to remove test JWT keys", exception);
        }
    }

    private String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }
}
