/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.properties.bean;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.internal.provider.ProducerAwareProperty;
import org.gradle.api.internal.provider.PropertyInternal;
import org.gradle.api.internal.tasks.properties.BeanPropertyContext;
import org.gradle.api.internal.tasks.properties.ParameterValidationContext;
import org.gradle.api.internal.tasks.properties.PropertyValue;
import org.gradle.api.internal.tasks.properties.PropertyVisitor;
import org.gradle.api.internal.tasks.properties.TypeMetadata;
import org.gradle.api.internal.tasks.properties.annotations.PropertyAnnotationHandler;
import org.gradle.api.provider.Provider;
import org.gradle.internal.Factory;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.reflect.PropertyMetadata;
import org.gradle.util.DeprecationLogger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

import static org.gradle.api.internal.tasks.properties.DefaultParameterValidationContext.propertyValidationMessage;

public abstract class AbstractNestedRuntimeBeanNode extends RuntimeBeanNode<Object> {
    protected AbstractNestedRuntimeBeanNode(@Nullable RuntimeBeanNode<?> parentNode, @Nullable String propertyName, Object bean, TypeMetadata typeMetadata) {
        super(parentNode, propertyName, bean, typeMetadata);
    }

    public void visitProperties(PropertyVisitor visitor, final Queue<RuntimeBeanNode<?>> queue, final RuntimeBeanNodeFactory nodeFactory, ParameterValidationContext validationContext) {
        TypeMetadata typeMetadata = getTypeMetadata();
        for (PropertyMetadata propertyMetadata : typeMetadata.getPropertiesMetadata()) {
            PropertyAnnotationHandler annotationHandler = typeMetadata.getAnnotationHandlerFor(propertyMetadata);
            String propertyName = getQualifiedPropertyName(propertyMetadata.getPropertyName());
            if (annotationHandler == null) {
                validationContext.recordValidationMessage(propertyValidationMessage(propertyName, "is not annotated with an input or output annotation"));
            } else if (annotationHandler.shouldVisit(visitor)) {
                PropertyValue value = new BeanPropertyValue(getBean(), propertyMetadata.getGetterMethod());
                annotationHandler.visitPropertyValue(propertyName, value, propertyMetadata, visitor, new BeanPropertyContext() {
                    @Override
                    public void addNested(String propertyName, Object bean) {
                        queue.add(nodeFactory.create(AbstractNestedRuntimeBeanNode.this, propertyName, bean));
                    }
                });
                for (String validationMessage : propertyMetadata.getValidationMessages()) {
                    validationContext.recordValidationMessage(propertyValidationMessage(propertyName, validationMessage));
                }

            }
        }
    }

    private static class BeanPropertyValue implements PropertyValue {
        private final Method method;
        private final Object bean;
        private final Supplier<Object> valueSupplier = Suppliers.memoize(new Supplier<Object>() {
            @Override
            @Nullable
            public Object get() {
                return DeprecationLogger.whileDisabled(new Factory<Object>() {
                    public Object create() {
                        try {
                            return method.invoke(bean);
                        } catch (InvocationTargetException e) {
                            throw UncheckedException.throwAsUncheckedException(e.getCause());
                        } catch (Exception e) {
                            throw new GradleException(String.format("Could not call %s.%s() on %s", method.getDeclaringClass().getSimpleName(), method.getName(), bean), e);
                        }
                    }
                });
            }
        });

        public BeanPropertyValue(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
            method.setAccessible(true);
        }

        @Override
        public void attachProducer(Task producer) {
            if (isProvider()) {
                Object value = valueSupplier.get();
                if (value instanceof ProducerAwareProperty) {
                    ((ProducerAwareProperty) value).attachProducer(producer);
                }
            }
        }

        @Override
        public void maybeFinalizeValue() {
            if (isProvider()) {
                Object value = valueSupplier.get();
                if (value instanceof PropertyInternal) {
                    ((PropertyInternal) value).finalizeValueOnReadAndWarnAboutChanges();
                }
            }
        }

        private boolean isProvider() {
            return Provider.class.isAssignableFrom(method.getReturnType());
        }

        @Nullable
        @Override
        public Object call() {
            Object value = valueSupplier.get();
            // Replace absent Provider with null.
            // This is required for allowing optional provider properties - all code which unpacks providers calls Provider.get() and would fail if an optional provider is passed.
            // Returning null from a Callable is ignored, and PropertyValue is a callable.
            if (value instanceof Provider && !((Provider<?>) value).isPresent()) {
                return null;
            }
            return value;
        }
    }
}
