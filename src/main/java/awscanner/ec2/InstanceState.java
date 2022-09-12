package awscanner.ec2;

import java.util.Optional;


public enum InstanceState {
    PENDING( 0 ),
    RUNNING( 16 ),
    SHUTTING_DOWN( 32 ),
    TERMINATED( 48 ),
    STOPPING( 64 ),
    STOPPED( 80 );

    private final int code;


    InstanceState( int code ) {
        this.code = code;
    }


    public int code() {
        return code;
    }


    public static Optional<InstanceState> findByCode( int code ) {
        for ( var is : values() ) {
            if ( is.code == code ) return Optional.of( is );
        }
        return Optional.empty();
    }
}
