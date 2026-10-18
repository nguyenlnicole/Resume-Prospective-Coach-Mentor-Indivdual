package com.pnbpaws.resume.mobile;

import java.net.URI;
import java.util.Locale;

final class EditionPolicy {
    static final String BETA = "https://resumep.pnbpaws.com";
    static final String COACH = "https://crp.pnbpaws.com";
    static final String LOCAL = "http://127.0.0.1:5173";
    static boolean trusted(String value, String edition) {
        return trusted(value, edition, false);
    }
    static boolean trusted(String value, String edition, boolean localPreviewAllowed) {
        try {
            URI uri = URI.create(value);
            URI selected = URI.create(edition);
            if (localPreviewAllowed && LOCAL.equals(edition))
                return "http".equalsIgnoreCase(uri.getScheme()) && "127.0.0.1".equals(uri.getHost())
                    && uri.getPort() == 5173 && uri.getUserInfo() == null;
            return (BETA.equals(edition) || COACH.equals(edition))
                && "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                && selected.getHost().equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (RuntimeException e) { return false; }
    }
    static String safeFilename(String value, boolean pdf) {
        String filename = value.replaceAll("[^A-Za-z0-9._-]", "-");
        if (filename.length() > 100) filename = filename.substring(0, 100);
        String suffix = pdf ? ".pdf" : ".docx";
        return filename.toLowerCase(Locale.ROOT).endsWith(suffix) ? filename : "Resume" + suffix;
    }
}
