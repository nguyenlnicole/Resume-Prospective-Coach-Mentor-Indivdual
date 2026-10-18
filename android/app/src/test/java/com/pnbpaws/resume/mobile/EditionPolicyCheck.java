package com.pnbpaws.resume.mobile;

public class EditionPolicyCheck {
    public static void main(String[] args) {
        require(EditionPolicy.trusted("https://resumep.pnbpaws.com/#home", EditionPolicy.BETA));
        require(EditionPolicy.trusted("https://crp.pnbpaws.com:443/#home", EditionPolicy.COACH));
        for (String url : new String[]{"https://crp.pnbpaws.com/", "http://resumep.pnbpaws.com/", "https://resumep.pnbpaws.com.evil.test/", "https://resumep.pnbpaws.com@evil.test/", "https://resumep.pnbpaws.com:8443/", "file:///secret", "javascript:alert(1)", "invalid"})
            require(!EditionPolicy.trusted(url, EditionPolicy.BETA));
        require(!EditionPolicy.trusted(EditionPolicy.LOCAL, EditionPolicy.LOCAL));
        require(EditionPolicy.trusted(EditionPolicy.LOCAL + "/?walkthrough=sample", EditionPolicy.LOCAL, true));
        for (String url : new String[]{"http://127.0.0.1:8080/", "http://127.0.0.1.evil.test:5173/", "http://user@127.0.0.1:5173/", "http://localhost:5173/", EditionPolicy.BETA})
            require(!EditionPolicy.trusted(url, EditionPolicy.LOCAL, true));
        require(!EditionPolicy.trusted(EditionPolicy.BETA, "https://admin.pnbpaws.com"));
        require(!EditionPolicy.safeFilename("../../resume.pdf", true).contains("/"));
        require(EditionPolicy.safeFilename("resume.docx", false).equals("resume.docx"));
        System.out.println("Edition routing and filename checks passed");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
}
