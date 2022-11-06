module com.udacity.imageservice {
    exports com.udacity.imageservice to com.udacity.securityservice;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    requires java.desktop;
    requires org.slf4j;
    opens com.udacity.imageservice to com.udacity.securityservice;

}