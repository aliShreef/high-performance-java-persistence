package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class IPv4TypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    private Event _event;

    @Override
    public void afterInit() {
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE INDEX ON event USING gist (ip inet_ops)");
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        _event = doInJPA(entityManager -> {
            entityManager.persist(new Event());

            Event event = new Event();
            event.setIp("192.168.0.123/24");
            entityManager.persist(event);

            return event;
        });
    }

    @Test
    public void testFindById() {
        Event updatedEvent = doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, _event.getId());

            assertEquals("192.168.0.123/24", event.getIp().getAddress());
            assertEquals("192.168.0.123", event.getIp().toInetAddress().getHostAddress());

            event.setIp("192.168.0.231/24");

            return event;
        });

        assertEquals("192.168.0.231/24", updatedEvent.getIp().getAddress());
    }

    @Test
    public void testJPQLQuery() {
        doInJPA(entityManager -> {
            List<Event> events = entityManager.createQuery("""
                select e
                from Event e
                where
                   ip is not null
                """, Event.class)
            .getResultList();

            Event event = events.get(0);
            assertEquals("192.168.0.123/24", event.getIp().getAddress());
        });
    }

    @Test
    public void testNativeQuery() {
        doInJPA(entityManager -> {
            List<Event> events = entityManager.createNativeQuery("""
                SELECT e.*
                FROM event e
                WHERE
                   e.ip && CAST(:network AS inet) = true
                """, Event.class)
            .setParameter("network", "192.168.0.1/24")
            .getResultList();

            assertTrue(
                events
                    .stream()
                    .map(Event::getIp)
                    .anyMatch(ip -> ip.getAddress().equals("192.168.0.123/24"))
            );
        });
    }


    @Test
    public void testJDBCQuery() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                try(PreparedStatement ps = connection.prepareStatement("""
                    SELECT *
                    FROM Event e
                    WHERE
                        e.ip && ?::inet = true
                    """
                )) {
                    ps.setObject(1, "192.168.0.1/24");
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()) {
                        Long id = rs.getLong(1);
                        String ip = rs.getString(2);
                        assertEquals("192.168.0.123/24", ip);
                    }
                }
            });
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "ip", columnDefinition = "inet")
        private Inet ip;

        public Long getId() {
            return id;
        }

        public Inet getIp() {
            return ip;
        }

        public void setIp(String address) {
            this.ip = new Inet(address);
        }
    }
}
