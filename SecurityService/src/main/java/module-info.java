module SecurityService {
    requires java.desktop;
    requires miglayout.swing;
    requires ImageService;

    requires java.prefs;
    requires java.sql;
    requires com.google.gson;
    requires guava;
    opens org.shifat.securityservices.data to com.google.gson;
}