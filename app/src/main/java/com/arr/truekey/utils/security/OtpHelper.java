package com.arr.truekey.utils.security;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

public class OtpHelper {

    public static String generateCode(String base32Secret) throws CodeGenerationException {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        long counter = timeProvider.getTime() / 30;
        return codeGenerator.generate(base32Secret, counter);
    }

    public static int secondsRemaining() {
        return 30 - (int) (System.currentTimeMillis() / 1000L % 30);
    }
}
