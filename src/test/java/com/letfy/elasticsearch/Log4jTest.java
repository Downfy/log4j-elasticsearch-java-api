/*
 * Copyright 2012 Letfy Team <admin@letfy.com>.
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
package com.letfy.elasticsearch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tran Anh Tuan <tuanta@letfy.com>
 */
public class Log4jTest {

    private Logger logger = LoggerFactory.getLogger(Log4jTest.class);

    @Test
    public void testLog4j() {
        logger.info("Log4j for Elastic Search");
    }
}
