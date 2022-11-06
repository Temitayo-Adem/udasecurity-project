module com.udacity.securityservice {
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires miglayout;
    requires com.google.common;
    requires com.udacity.imageservice;
    opens com.udacity.securityservice.data to com.google.gson;



}