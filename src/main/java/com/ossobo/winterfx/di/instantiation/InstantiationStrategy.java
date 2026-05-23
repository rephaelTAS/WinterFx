package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.di.scanner.models.BeanDefinition;

public interface InstantiationStrategy {

    Object instantiate(BeanDefinition definition) throws Exception;
    boolean canHandle(BeanDefinition definition);
}