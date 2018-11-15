package org.servantscode.ministry.db;

import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.ministry.Ministry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class MinistryDB extends DBAccess {

    public int getCount(String search) {
        String sql = format("Select count(1) from ministries%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery() ){

            return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministries with names containing " + search, e);
        }
    }

    public List<Ministry> getMinistries(String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM ministries%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processMinistryResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministries with names containing " + search, e);
        }
    }

    public List<String> getMinistryNames(String search, int count) {
        String sql = format("SELECT name FROM ministries%s", optionalWhereClause(search));
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            try ( ResultSet rs = stmt.executeQuery()){
                List<String> names = new ArrayList<>();

                while(rs.next())
                    names.add(rs.getString(1));

                names.sort(new AutoCompleteComparator(search));
                return (count < names.size())? names: names.subList(0, count);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministry names containing " + search, e);
        }
    }

    public Ministry getMinistry(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ministries WHERE id=?")
        ) {

            stmt.setInt(1, id);
            List<Ministry> results = processMinistryResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministry by id: " + id, e);
        }
    }

    public void create(Ministry ministry) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO ministries(name, description) values (?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, ministry.getName());
            stmt.setString(2, ministry.getDescription());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create ministry: " + ministry.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    ministry.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create ministry: " + ministry.getName(), e);
        }
    }

    public void update(Ministry ministry) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE ministries SET name=?, description=? WHERE id=?")
        ){

            stmt.setString(1, ministry.getName());
            stmt.setString(2, ministry.getDescription());
            stmt.setInt(3, ministry.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update ministry: " + ministry.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update ministry: " + ministry.getName(), e);
        }
    }

    public boolean delete(Ministry ministry) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM ministries WHERE id=?")
        ){

            stmt.setInt(1, ministry.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete ministry: " + ministry.getName(), e);
        }
    }

    // ----- Private ------
    private List<Ministry> processMinistryResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Ministry> ministries = new ArrayList<>();
            while(rs.next()) {
                Ministry ministry = new Ministry(rs.getInt("id"), rs.getString("name"));
                ministry.setDescription(rs.getString("description"));
                ministries.add(ministry);
            }
            return ministries;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search)? format(" WHERE name ILIKE '%%%s%%'", search) : "";
    }
}