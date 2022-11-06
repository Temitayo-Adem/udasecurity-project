package com.udacity.imageservice;

import java.awt.image.BufferedImage;

public interface IService {
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
