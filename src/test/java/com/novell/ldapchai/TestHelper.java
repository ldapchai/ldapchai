package com.novell.ldapchai;

import com.novell.ldapchai.cr.ChaiCrFactoryTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TestHelper
{
    public static String readResourceFile( final Class callingClass, final String name )
            throws IOException
    {
        final InputStream inputStream = callingClass.getResourceAsStream( name );
        final StringBuilder textBuilder = new StringBuilder();
        try ( Reader reader = new BufferedReader( new InputStreamReader( inputStream, Charset.forName( StandardCharsets.UTF_8.name() ) ) ) )
        {
            int c;
            while ( ( c = reader.read() ) != -1)
            {
                textBuilder.append( (char) c );
            }
        }
        return textBuilder.toString();
    }
}
