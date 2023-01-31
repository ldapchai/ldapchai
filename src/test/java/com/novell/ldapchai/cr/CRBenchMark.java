/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai.cr;

import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
@Fork( jvmArgs =
        {
                "-Xms1G", "-Xmx1G"
        }, value = 1 )
@Warmup( iterations = 3 )
@Measurement( iterations = 2 )
public class CRBenchMark
{
    @Param( {
            "MD5",
            "SHA1",
            "SHA1_SALT",
            "SHA256_SALT",
            "SHA256_SALT",
            "PBKDF2",
            "PBKDF2_SHA256",
            "PBKDF2_SHA512"
    } )
    public String hashFormatParam;

    public static void main( final String[] args ) throws RunnerException
    {
        final Options opt = new OptionsBuilder()
                .include( CRBenchMark.class.getSimpleName() )
                .forks( 1 )
                .build();

        new Runner( opt ).run();
    }


    @Benchmark
    public void testResponseSetAnswers()
            throws Exception
    {
        final Answer.FormatType formatType = Answer.FormatType.valueOf( hashFormatParam );

        final Map<Challenge, String> challengeAnswerMap = new HashMap<>();
        challengeAnswerMap.put(
                new ChaiChallenge(
                        true,
                        "challenge1",
                        0,
                        255,
                        true,
                        0,
                        false ),
                "response1" );

        final ChaiConfiguration chaiConfiguration = ChaiConfiguration.builder( "ldap://1", "bindDN", "bindPW" )
                .setSetting( ChaiSetting.CR_DEFAULT_FORMAT_TYPE, formatType.name() )
                .build();

        final ResponseSet responseSet = ChaiCrFactory.newChaiResponseSet(
                challengeAnswerMap, Locale.US, 0, chaiConfiguration, "test-response-set" );

        final boolean tested = responseSet.test( challengeAnswerMap );
        Assertions.assertTrue( tested );
        //System.out.println( responseSet.stringValue() );
    }
}
