package awscanner;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;


/**
 * Abstraction layer to hide whether or not color output is enabled.
 */
public interface ColorWriter {
    Color BLUE = Color.BLUE;
    Color GREEN = Color.GREEN;
    Color NONE = Color.NONE;
    Color RED = Color.RED;
    Color YELLOW = Color.YELLOW;

    enum Color {
        BLUE( Attribute.BLUE_TEXT() ),
        GREEN( Attribute.GREEN_TEXT() ),
        NONE( Attribute.NONE() ),
        RED( Attribute.RED_TEXT() ),
        YELLOW( Attribute.YELLOW_TEXT() );

        private final AnsiFormat format;

        Color( Attribute attribute ) {
            this.format = new AnsiFormat( attribute );
        }
    }

    void print( String text, Color color );
    void println( String text, Color color );

    default void print( String text ) {
        print( text, NONE );
    }
    default void println( String text ) {
        println( text, NONE );
    }


    static ColorWriter create( boolean allow_color ) {
        return allow_color ? new JColorWriter() : new NoColorWriter();
    }

    class JColorWriter implements ColorWriter {
        @Override
        public void print( String text, Color color ) {
            System.out.print( Ansi.colorize( text, color.format ) );
        }

        @Override
        public void println( String text, Color color ) {
            System.out.println( Ansi.colorize( text, color.format ) );
        }
    }

    class NoColorWriter implements ColorWriter {
        @Override
        public void print( String text, Color color ) {
            System.out.print( text );
        }

        @Override public void println( String text, Color color ) {
            System.out.println( text );
        }
    }
}


