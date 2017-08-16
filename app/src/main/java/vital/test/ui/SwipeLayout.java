package vital.test.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import vital.test.R;

public class SwipeLayout extends RelativeLayout implements View.OnTouchListener {

    private static final String DEBUG_TAG = SwipeLayout.class.getName( );

    private int imageId;
    private int containerId;

    private Float prevYval;
    private Float minHeight;
    private Float maxHeight;

    private ImageView triggerContainer;
    private ScrollView contentContainer;


    boolean canBeSwipeProcessed = false;
    boolean canBeIntercepted = true;

    public SwipeLayout( Context context ) {
        super( context );
        init( );

    }

    public SwipeLayout( Context context, @Nullable AttributeSet attrs ) {
        super( context, attrs );
        init( );
    }

    public SwipeLayout( Context context, @Nullable AttributeSet attrs, int defStyleAttr ) {
        super( context, attrs, defStyleAttr );
        init( );
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwipeLayout( Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes ) {
        super( context, attrs, defStyleAttr, defStyleRes );
        init( );
    }

    private void init( ) {

        triggerContainer = new android.support.v7.widget.AppCompatImageView( this.getContext( ) ) {
            @Override
            public boolean onTouchEvent( MotionEvent event ) {
                return true;
            }
        };
        triggerContainer.setImageDrawable( ContextCompat.getDrawable( getContext( ), R.drawable.line ) );

        minHeight = 20 * getResources( ).getDisplayMetrics( ).density;
        imageId = View.generateViewId( );

        RelativeLayout.LayoutParams params = new LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, minHeight.intValue( ) );
        params.addRule( RelativeLayout.ALIGN_PARENT_BOTTOM );

        triggerContainer.setLayoutParams( params );
        triggerContainer.setId( imageId );
        triggerContainer.setBackgroundColor( ResourcesCompat.getColor( getResources( ), android.R.color.holo_green_light, null ) );
        triggerContainer.setOnTouchListener( this );

        contentContainer = new ScrollView( this.getContext( ) ) {
            @Override
            public boolean onTouchEvent( MotionEvent ev ) {
                return true;
            }
        };
        contentContainer.setOnTouchListener( this );
        contentContainer.setId( View.generateViewId( ) );
        this.addView( contentContainer );


        this.post( new Runnable( ) {
            @Override
            public void run( ) {
                for ( int i = 0; i < SwipeLayout.this.getChildCount( ); i++ ) {
                    View child = SwipeLayout.this.getChildAt( i );
                    if ( child.getId( ) != triggerContainer.getId( ) && child.getId( ) != contentContainer.getId( ) ) {
                        SwipeLayout.this.removeView( child );
                        contentContainer.addView( child );
                    }
                }
            }
        } );

        this.addView( triggerContainer );

        this.setOnTouchListener( this );
        this.setBackgroundColor( ResourcesCompat.getColor( getResources( ), android.R.color.holo_red_dark, null ) );

        if ( this.getId( ) == NO_ID ) {
            containerId = View.generateViewId( );
            this.setId( containerId );
        } else {
            containerId = this.getId( );
        }
    }

    @Override
    public boolean onTouch( View v, MotionEvent event ) {

        int action = MotionEventCompat.getActionMasked( event );

        switch ( action ) {

            case ( MotionEvent.ACTION_DOWN ):
                Log.d( DEBUG_TAG, "Action was DOWN  view ID = " + v.getId( ) + " image ID = " + imageId );
                if ( v.getId( ) == triggerContainer.getId( ) ) {
                    canBeSwipeProcessed = true;
                } else if ( v.getId( ) == contentContainer.getId( ) ){
                    canBeIntercepted = false;
                }

                if ( maxHeight == null )
                    maxHeight = (float) this.getRootView( ).getHeight( );

                return true;
            case ( MotionEvent.ACTION_UP ):
                Log.d( DEBUG_TAG, "Action was UP" );
                canBeSwipeProcessed = false;
                canBeIntercepted = true;
                prevYval = null;
                return true;

            case ( MotionEvent.ACTION_MOVE ):
                //Log.d( DEBUG_TAG, "Action was MOVE - canBeSwipeProcessed " + canBeSwipeProcessed + " view ID = " + v.getId( ) + "  container id " + containerId );

                if ( v.getId( ) == containerId && canBeSwipeProcessed ) {

                    ViewGroup.LayoutParams params = this.getLayoutParams( );

                    if ( params.height == ViewPager.LayoutParams.MATCH_PARENT || params.height == ViewPager.LayoutParams.WRAP_CONTENT )
                        params.height = this.getMeasuredHeight( );

                    prevYval = prevYval == null ? event.getY( ) : prevYval;

                    params.height += event.getY( ) - prevYval;

                    if ( params.height < minHeight ) {
                        params.height = minHeight.intValue( );
                        contentContainer.setVisibility( GONE );
                    } else if ( params.height > maxHeight ) {
                        params.height = maxHeight.intValue( );
                    } else {
                        if ( contentContainer.getVisibility( ) == GONE )
                            contentContainer.setVisibility( VISIBLE );
                    }

                    prevYval = event.getY( );

                    Log.d( DEBUG_TAG, "Action was MOVE - Height " + params.height );
                    this.setLayoutParams( params );

                }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent( MotionEvent ev ) {
        boolean res = MotionEventCompat.getActionMasked( ev ) == MotionEvent.ACTION_MOVE && canBeIntercepted;
        Log.d( DEBUG_TAG, "onInterceptTouchEvent result = " + res );
        return res;
    }

    /**
     * future updates
     */
    enum SwipeDirection {
        FROM_TOP
    }

    /**
     * position to automatic swipe state
     */
    enum SwipePosition {
        TO_BOTOM, // to bottom of parent
        TO_POSITION, // px from top
        TO_END_OF, // id of layout
        TO_TOP; // collapse view
    }

    /**
     *
     */


}
