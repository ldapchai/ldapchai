package com.novell.ldapchai.cr;

import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import org.junit.Assert;
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

@BenchmarkMode( Mode.Throughput )
@OutputTimeUnit( TimeUnit.MILLISECONDS)
@State( Scope.Benchmark )
@Fork( jvmArgs = {"-Xms256M", "-Xmx256M"})
@Warmup(iterations = 3)
@Measurement(iterations = 8)
public class CRBenchMark
{

    @Param({
            "MD5",
            "SHA1",
            "SHA1_SALT",
            "SHA256_SALT",
            "SHA256_SALT",
            "PBKDF2",
            "PBKDF2_SHA256",
            "PBKDF2_SHA512"
    })
    public String hashFormatParam;

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder()
                .include(CRBenchMark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }


    @Benchmark
    public void testResponseSetAnswers()
            throws Exception
    {
        final Answer.FormatType formatType = Answer.FormatType.valueOf( hashFormatParam );

        final Map<Challenge, String> challengeAnswerMap = new HashMap<>();
        challengeAnswerMap.put(
                new ChaiChallenge( true, "challenge1", 0, 255, true, 0, false ),
                "response1" );

        final ChaiConfiguration chaiConfiguration = ChaiConfiguration.builder( "ldap://1", "bindDN", "bindPW" )
                .setSetting( ChaiSetting.CR_DEFAULT_FORMAT_TYPE, formatType.name() )
                .build();

        final ResponseSet responseSet = ChaiCrFactory.newChaiResponseSet(
                challengeAnswerMap, Locale.US, 0, chaiConfiguration, "test-response-set" );

        final boolean tested = responseSet.test( challengeAnswerMap );
        Assert.assertTrue( tested );
        //System.out.println( responseSet.stringValue() );
    }
}
