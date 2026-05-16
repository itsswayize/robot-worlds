package za.co.wethinkcode.persist.orm;

import java.util.List;

import net.lemnik.eodsql.BaseQuery;
import net.lemnik.eodsql.Select;
import net.lemnik.eodsql.Update;

public interface ProductDAI extends BaseQuery
{
    //<editor-fold desc="QUERY METHODS">
    /** A db query with NO parameters */
    @Select(
            "SELECT p.id, p.name, s.name style "
                    + "FROM products p, styles s "
                    + "WHERE p.style_id = s.id"
    )
    List<ProductDO> getAllProducts();

    /** A db Query with one parameter */
    @Select(
            "SELECT p.id, p.name, s.name style "
                    + "FROM products p, styles s "
                    + "WHERE p.style_id = s.id AND p.id = ?{1}"
    )
    ProductDO getProduct( int productId );

    /** A query with 2 parameters */
    @Select(
        "SELECT p.id pId, p.name pName, i.id iId, i.name iName, i.type iType, q.qty, q.units  "
        + "FROM products p, ingredients i, recipe_quantities q "
        + "WHERE p.id = ?{1} "
        + "     AND i.id = ?{2} "
        + "     AND q.product_id = p.id "
        + "     AND q.ingredient_id = i.id"
    )
    RecipeIngredientDO getIngredientDetails( int productId, int ingredientId );

    @Select( "SELECT count(*) FROM products" )
    public int getNumberOfProducts();
    //</editor-fold>

    //<editor-fold desc="DATA INSERT METHODS">
    /** Create a new Product... */
    @Update(
        "INSERT INTO products (id, name, style_id  ) "
            + "VALUES (?{1}, ?{2}, ?{3})"
    )
    void addProduct( int productId, String name, int styleId );

    /** Create a new Product... */
    @Update(
        "INSERT INTO products (name, style_id  ) "
            + "VALUES (?{1.name}, ?{2})"
    )
    void addProduct( ProductDO product, int styleId );

    /** Create a new Product, but rely on auto-increment constraint for the `id` primary key */
    @Update(
        "INSERT INTO products (name, style_id) "
            + "VALUES (?{1}, ?{2})"
    )
    int addProduct( String name, int styleId );
    //</editor-fold>

    //<editor-fold desc="DATA UPDATE METHODS">
    @Update(
        "UPDATE products "
            + "SET name = ?{2} "
            + "WHERE id = ?{1} "
    )
    void updateProductName( int productId, String newName );

    @Update(
        "UPDATE products "
            + "SET name = ?{1.name} "
            + "WHERE id = ?{1.primaryKey} "
    )
    void updateProductName( ProductDO p );
    //</editor-fold>

}