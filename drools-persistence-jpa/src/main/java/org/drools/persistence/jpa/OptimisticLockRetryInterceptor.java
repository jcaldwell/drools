/*
 * Copyright 2013 JBoss Inc
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

package org.drools.persistence.jpa;

import org.drools.core.command.impl.AbstractInterceptor;
import org.kie.api.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor that is capable of retrying command execution. It is intended to retry only if right exception
 * has been thrown. By default it will look for <code>org.hibernate.StaleObjectStateException</code> and only
 * then attempt to retry.
 * Since this is Hibernate specific class another can be given as system property to override default. Name of the
 * system property <code>org.kie.optlock.exclass</code> and its value should be fully qualified class name of the
 * exception that indicates OptimisticLocking.
 * By default it will:
 * <ul>
 *  <li>Retry 3 times</li>
 *  <li>First retry will be attempted after 50 milliseconds</li>
 *  <li>next retries will be calculated as last sleep time multiplied by a factor (default factor is 4)</li>
 * </ul>
 * In case all retries failed origin exception will be thrown.
 *
 */
public class OptimisticLockRetryInterceptor extends AbstractInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockRetryInterceptor.class);

    private int retries = 3;
    private long delay = 50;
    private long delayFactor = 4;

    protected Class<?> targetExceptionClass;

    public OptimisticLockRetryInterceptor() {
        String clazz = System.getProperty("org.kie.optlock.exclass", "org.hibernate.StaleObjectStateException");
        try {
            targetExceptionClass = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            logger.error("Optimistic locking exception class not found {}", clazz, e);
        }
    }

    @Override
    public <T> T execute(Command<T> command) {
        int attempt = 1;
        long sleepTime = delay;
        RuntimeException originException = null;

        while (attempt <= retries) {
            if (attempt > 1) {
                logger.trace("retrying (attempt {})...", attempt);
            }
            try {

                return executeNext(command);

            } catch (RuntimeException ex) {
                logger.trace(ex.getClass().getSimpleName() + " caught in " + this.getClass().getSimpleName() + ": " + ex.getMessage());
                if (!isCausedByOptimisticLockingFailure(ex)) {
                    throw ex;
                }
                attempt++;
                logger.trace("Command failed due to optimistic locking {} waiting {} millis before retry", ex, sleepTime);
                // save origin exception in case it needs to be rethrown
                if (originException == null) {
                    originException = ex;
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e1) {
                    logger.trace("retry sleeping got interrupted");
                }

                sleepTime *= delayFactor;
            }
        }
        logger.warn("Retry failed after {} attempts", attempt);
        throw originException;

    }

    protected boolean isCausedByOptimisticLockingFailure(Throwable throwable) {
        if (targetExceptionClass == null) {
            logger.warn("targetExceptionClass not configured, the retry interceptor is disabled.");
            return false;
        }

        while (throwable != null) {
            if (targetExceptionClass.isAssignableFrom(throwable.getClass())) {
                return true;
            } else {
                throwable = throwable.getCause();
            }
        }

        return false;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getDelayFactor() {
        return delayFactor;
    }

    public void setDelayFactor(long delayFactor) {
        this.delayFactor = delayFactor;
    }

    public Class<?> getTargetExceptionClass() {
        return targetExceptionClass;
    }

    public void setTargetExceptionClass(Class<?> targetExceptionClass) {
        this.targetExceptionClass = targetExceptionClass;
    }

}
