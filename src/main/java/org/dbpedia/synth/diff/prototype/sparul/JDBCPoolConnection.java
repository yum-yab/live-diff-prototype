package org.dbpedia.synth.diff.prototype.sparul;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.dbpedia.synth.diff.prototype.helper.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Manipulates a JDBC Connection pool to BoneCP
 */
public final class JDBCPoolConnection {
    //Initializing the Logger
    //private static final Logger logger = LoggerFactory.getLogger(JDBCPoolConnection.class);

    private static volatile BoneCP connectionPool;



    /**
     * Instantiates a new JDBC pool connection.
     */
    private JDBCPoolConnection() {
    }

    /**
     * Initializes the pool from a property file
     * (need to move `Globals` out of here)
     */
    private static void initConnection() {

        try {
            BoneCPConfig config = new BoneCPConfig();
            Class.forName(Global.getOptions().get("Store.class"));
            config.setJdbcUrl(Global.getOptions().get("Store.dsn"));
            config.setUsername(Global.getOptions().get("Store.user"));
            config.setPassword(Global.getOptions().get("Store.pw"));
            connectionPool = new BoneCP(config); // setup the connection pool
        } catch (Exception e) {
            //logger.error("Could not initialize Triple-Store connection! Exiting ...", e);
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Gets a connection from the pool
     *
     * @return a connection
     * @throws SQLException the sQL exception
     */
    public static Connection getPoolConnection() throws SQLException {
        if (connectionPool == null) {
            synchronized (JDBCPoolConnection.class) {
                if (connectionPool == null) {
                    initConnection();
                }
            }
        }
        return connectionPool.getConnection();
    }


    /**
     * Shutdown the connection pool.
     */
    public static void shutdown() {
        if (connectionPool != null) {
            try {
                connectionPool.close();
            } catch (Exception e) {
                //logger.error("Cannot close connection pool", e);
                System.exit(1);
            }
        }

    }


}
