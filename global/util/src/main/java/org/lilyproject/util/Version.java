package org.lilyproject.util;

import org.lilyproject.util.io.Closer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {
    public static String readVersion(String groupId, String artifactId) {
        String propPath = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        InputStream is = Version.class.getResourceAsStream(propPath);
        if (is != null) {
            Properties properties = new Properties();
            try {
                properties.load(is);
                String version = properties.getProperty("version");
                if (version != null) {
                    return version;
                }
            } catch (IOException e) {
                // ignore
            }
            Closer.close(is);
        }

        return "undetermined (please report this as bug)";
    }
}
