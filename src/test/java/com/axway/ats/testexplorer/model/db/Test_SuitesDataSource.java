/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.testexplorer.model.db;

import org.junit.Assert;
import org.junit.Test;

public class Test_SuitesDataSource {

    @Test
    public void test1() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.product.tests.perf",
                                                                        "com.product.tests.functional" } );

        Assert.assertEquals( result[0], "perf" );
        Assert.assertEquals( result[1], "functional" );

    }

    @Test
    public void test2() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.product.tests.functional",
                                                                        "com.product.tests.perf" } );

        Assert.assertEquals( result[0], "functional" );
        Assert.assertEquals( result[1], "perf" );

    }

    @Test
    public void test3() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.product.tests.perf",
                                                                        "com.product.tests.perf.one" } );

        Assert.assertEquals( result[0], "perf" );
        Assert.assertEquals( result[1], "perf.one" );
    }

    @Test
    public void test4() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.product.tests.perf.one",
                                                                        "com.product.tests.perf" } );

        Assert.assertEquals( result[0], "perf.one" );
        Assert.assertEquals( result[1], "perf" );
    }

    @Test
    public void test5() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.functional.two",
                                                                        "com.perf.one.three" } );

        Assert.assertEquals( result[0], "functional.two" );
        Assert.assertEquals( result[1], "perf.one.three" );
    }

    @Test
    public void test6() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.perf.one.three",
                                                                        "com.functional.two" } );

        Assert.assertEquals( result[0], "perf.one.three" );
        Assert.assertEquals( result[1], "functional.two" );
    }

    @Test
    public void test7() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.product.tests.perf",
                                                                        "com.product.tests.perf" } );

        Assert.assertEquals( result[0], "perf" );
        Assert.assertEquals( result[1], "perf" );
    }

    @Test
    public void test8() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "", "" } );

        Assert.assertEquals( result[0], "" );
        Assert.assertEquals( result[1], "" );
    }

    @Test
    public void test9() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.axway.ats.log.autodb", "",
                                                                        "com.axway.ats.log.autodb" } );

        Assert.assertEquals( result[0], "com.axway.ats.log.autodb" );
        Assert.assertEquals( result[1], "" );
        Assert.assertEquals( result[2], "com.axway.ats.log.autodb" );
    }

    @Test
    public void test10() {

        String[] result = SuitesDataSource.parsePackages( new String[]{ "com.axway.ats.log.autodb", "" } );

        Assert.assertEquals( result[0], "com.axway.ats.log.autodb" );
        Assert.assertEquals( result[1], "" );
    }

}
