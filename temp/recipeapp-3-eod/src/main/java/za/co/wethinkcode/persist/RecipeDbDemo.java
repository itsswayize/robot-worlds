package za.co.wethinkcode.persist;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import net.lemnik.eodsql.QueryTool;
import za.co.wethinkcode.persist.orm.ProductDAI;
import za.co.wethinkcode.persist.orm.ProductDO;
import za.co.wethinkcode.persist.orm.RecipeIngredientDO;

/**
 * RecipeDbDemo, but this time with PreparedStatements.
 */
public class RecipeDbDemo
{
    public static final String DISK_DB_URL_PREFIX = "jdbc:sqlite:";

    public static final String SEPARATOR = "\t";

    public static void main( String[] args ) {
        final RecipeDbDemo app = new RecipeDbDemo( args );
    }

    private String dbUrl = null;

    private RecipeDbDemo( String[] args ) {
        processCmdLineArgs( args );
        try( Connection connection = DriverManager.getConnection( dbUrl )){
            useTheDb( connection );
        }catch( SQLException e ){
            throw new RuntimeException( e );
        }
    }

    private void useTheDb( final Connection db )
        throws SQLException
    {
        final ProductDAI productQuery = QueryTool.getQuery( db, ProductDAI.class );

        readData( productQuery );
        createData( productQuery );
        updateData( productQuery );
        deleteData( productQuery );
    }

    private void readData( final ProductDAI dai )
        throws SQLException
    {
        final int productCount = dai.getNumberOfProducts();
        display( Integer.toString( productCount ) + " PRODUCTS." );

        final List<ProductDO> allProducts = dai.getAllProducts();

        display( "\nALL PRODUCTS:" );
        allProducts.forEach( p -> displayProduct( p ) );

        display( "\nSELECTED PRODUCTS:" );
        for( int i : new int[]{4, 2, 42} ){
            displayProduct( dai.getProduct( i ));
        }

        display( "\nSOME INGREDIENTS:" );
        for( int i: new int[]{ 1, 2, 3 } ){
            for( int j: new int[]{ 1, 3 } ){
                RecipeIngredientDO d = dai.getIngredientDetails( i, j );
                if( d == null ){
                    display( SEPARATOR + "Product " + i
                        + " doesn't use ingredient " + j );
                }else{
                    display( SEPARATOR + d.productId
                        + SEPARATOR + d.productName
                        + " USES " + d.qty + " " + d.units
                        + " OF INGREDIENT " + d.ingredientId
                        + " " + d.ingredientName + " " + d.ingredientType
                    );
                }
            }
        }
    }

    private void createData( final ProductDAI dai )
        throws SQLException
    {
        display( "\n\nCREATING NEW RECORDS: " );

        dai.addProduct( 42, "Leeuwenbosch Lager", 3 );
        displayAllProducts( dai );

        final ProductDO aProduct = new ProductDO( "Millwood Gold" );
        dai.addProduct( aProduct, 3 );
        displayAllProducts( dai );

        int n = dai.addProduct( "Wildside Blonde", 2 );
        displayAllProducts( dai );
        display( n + " rows added" );
    }

    private void updateData( final ProductDAI dai )
        throws SQLException
    {
        display( "\n\nUPDATING DATA:" );

        ProductDO p = new ProductDO();
        p.setPrimaryKey( 42 );
        p.setName( "Elephant P" );
        dai.updateProductName( p );
        displayAllProducts( dai );
    }

    private void deleteData( final ProductDAI dai )
        throws SQLException
    {
        throw new UnsupportedOperationException( "Over to you! ;)" );
    }

    private void displayAllProducts( final ProductDAI dai ) {
        display( "All Products:" );
        final List<ProductDO> allProducts = dai.getAllProducts();
        allProducts.forEach( p -> displayProduct( p ) );
    }

    private void displayProduct( ProductDO p ){
        if( p == null ){
            display( SEPARATOR + "errrrr..." );
        }else{
            display( SEPARATOR + p.getPrimaryKey() + SEPARATOR + p.getName() + SEPARATOR + p.getStyle() );
        }
    }

    private void display( String s ){
        System.out.println( s );
    }

    private void processCmdLineArgs( String[] args ) {
        if( args.length == 2 && args[ 0 ].equals( "-f" ) ){
            final File dbFile = new File( args[ 1 ] );
            if( dbFile.exists() ){
                dbUrl = DISK_DB_URL_PREFIX + args[ 1 ];
            }else{
                throw new IllegalArgumentException( "Database file " + dbFile.getName() + " not found." );
            }
        }else{
            throw new RuntimeException( "Expected arguments '-f filenanme' but didn't find it." );
        }
    }
}