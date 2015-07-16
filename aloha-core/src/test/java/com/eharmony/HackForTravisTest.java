package com.eharmony;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Taken from https://github.com/JohnLangford/vowpal_wabbit/blob/master/java/src/main/java/vw/jni/NativeUtils.java
 * to see what the value is in Travis CI.
 *
 * TODO: Change POM back to not redirect output after removing this...
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class HackForTravisTest {

    @Test
    public void printLibraryInfoTest() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("/proc/version"));

        StringBuilder b = new StringBuilder();
        for (String line; null != (line = reader.readLine()); ) {
            b.append(line).append("\n");
        }
        reader.close();


        System.out.println(b.toString());
        System.out.println(loadOSDependentLibrary("/vw_jni", ".lib"));
    }

    private static String getDistroName() throws IOException {
        Pattern distroRegex = Pattern.compile("[^(]+\\([^(]+\\([^(]+\\(([A-Za-z\\s]+).*");
        BufferedReader reader = new BufferedReader(new FileReader("/proc/version"));
        String distro;
        try {
            Matcher line = distroRegex.matcher(reader.readLine());
            distro = line.matches() ? line.group(1) : null;
        }
        finally {
            reader.close();
        }
        return distro;
    }

    /**
     * Because JNI requires dynamic linking the version of the linux distro matters.  This will attempt to find
     * the correct version of the linux distro.  Note that this right now tries to find if this is either
     * Ubuntu or not, and if it's not then it assumes CentOS.  I know this is not correct for all Linux distros
     * but hopefully this will work for most.
     * @return The linux distro and version
     * @throws IOException
     */
    private static String getLinuxDistro() throws IOException {
        BufferedReader reader = null;
        String release = null;
        String distro = getDistroName();
        try {
            Process process = Runtime.getRuntime().exec("lsb_release -r");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern releasePattern = Pattern.compile("Release:\\s*(\\d+).*");
            Matcher matcher;
            while ((line = reader.readLine()) != null) {
                matcher = releasePattern.matcher(line);
                if (matcher.matches()) {
                    release = matcher.group(1);
                }
            }
        }
        finally {
            reader.close();
        }
        if (distro == null || release == null) {
            throw new UnsupportedEncodingException("Linux distro does not support lsb_release, cannot determine version, distro: " + distro + ", release: " + release);
        }

        return distro.trim().replaceAll(" ", "_") + "." + release;
    }

    private static String getOsFamily() throws IOException {
        final String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("mac")) {
            return "Darwin";
        }
        else if (osName.toLowerCase().contains("linux")) {
            return getLinuxDistro();
        }
        throw new IllegalStateException("Unsupported operating system " + osName);
    }

    public static String loadOSDependentLibrary(String path, String suffix) throws IOException {
        String osFamily = getOsFamily();
        String osDependentLib = path + "." + osFamily + "." + System.getProperty("os.arch") + suffix;
        return osDependentLib;
    }
}
