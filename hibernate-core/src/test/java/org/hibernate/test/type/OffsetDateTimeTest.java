/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Query;
import org.hibernate.type.OffsetDateTimeType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10372")
public class OffsetDateTimeTest extends AbstractJavaTimeTypeTest<OffsetDateTime, OffsetDateTimeTest.EntityWithOffsetDateTime> {

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int year, int month, int day,
				int hour, int minute, int second, int nanosecond, String offset, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, year, month, day, hour, minute, second, nanosecond, offset );
		}
	}

	@Parameterized.Parameters(name = "{1}-{2}-{3}T{4}:{5}:{6}.{7}[{8}] {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				// Not affected by HHH-13266
				.add( 2017, 11, 6, 19, 19, 1, 0, "+10:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+07:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+00:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-02:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-06:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-08:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+10:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+07:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 500, "+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+00:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-02:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-06:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-08:00", ZONE_PARIS )
				.add( 1970, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_GMT )
				.add( 1970, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_GMT )
				.add( 1970, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_GMT )
				.add( 1900, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_GMT )
				.add( 1900, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_GMT )
				.add( 1900, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_GMT )
				.add( 1900, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
				.add( 1900, 1, 1, 0, 9, 21, 0, "+00:09:21", ZONE_PARIS )
				.add( 1900, 1, 1, 0, 19, 32, 0, "+00:19:32", ZONE_PARIS )
				.add( 1900, 1, 1, 0, 19, 32, 0, "+00:19:32", ZONE_AMSTERDAM )
				// Affected by HHH-13266
				.add( 1892, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
				.add( 1900, 1, 1, 0, 9, 20, 0, "+00:09:21", ZONE_PARIS )
				.add( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_PARIS )
				.add( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_AMSTERDAM )
				.add( 1600, 1, 1, 0, 0, 0, 0, "+00:19:32", ZONE_AMSTERDAM )
				.build();
	}

	private final int year;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;
	private final String offset;

	public OffsetDateTimeTest(EnvironmentParameters env, int year, int month, int day,
			int hour, int minute, int second, int nanosecond, String offset) {
		super( env );
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
		this.offset = offset;
	}

	@Override
	protected Class<EntityWithOffsetDateTime> getEntityType() {
		return EntityWithOffsetDateTime.class;
	}

	@Override
	protected EntityWithOffsetDateTime createEntityForHibernateWrite(int id) {
		return new EntityWithOffsetDateTime( id, getOriginalOffsetDateTime() );
	}

	private OffsetDateTime getOriginalOffsetDateTime() {
		return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.of( offset ) );
	}

	@Override
	protected OffsetDateTime getExpectedPropertyValueAfterHibernateRead() {
		return getOriginalOffsetDateTime().atZoneSameInstant( ZoneId.systemDefault() ).toOffsetDateTime();
	}

	@Override
	protected OffsetDateTime getActualPropertyValue(EntityWithOffsetDateTime entity) {
		return entity.value;
	}

	@Override
	protected void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex) throws SQLException {
		statement.setTimestamp( parameterIndex, getExpectedJdbcValueAfterHibernateWrite() );
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
		LocalDateTime dateTimeInDefaultTimeZone = getOriginalOffsetDateTime().atZoneSameInstant( ZoneId.systemDefault() )
				.toLocalDateTime();
		return new Timestamp(
				dateTimeInDefaultTimeZone.getYear() - 1900, dateTimeInDefaultTimeZone.getMonthValue() - 1,
				dateTimeInDefaultTimeZone.getDayOfMonth(),
				dateTimeInDefaultTimeZone.getHour(), dateTimeInDefaultTimeZone.getMinute(),
				dateTimeInDefaultTimeZone.getSecond(),
				dateTimeInDefaultTimeZone.getNano()
		);
	}

	@Override
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		return resultSet.getTimestamp( columnIndex );
	}

	@Test
	public void testRetrievingEntityByOffsetDateTime() {
		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.persist( new EntityWithOffsetDateTime( 1, getOriginalOffsetDateTime() ) );
			} );
			Consumer<OffsetDateTime> checkOneMatch = expected -> inSession( s -> {
				Query query = s.createQuery( "from " + ENTITY_NAME + " o where o.value = :date" );
				query.setParameter( "date", expected, OffsetDateTimeType.INSTANCE );
				List<EntityWithOffsetDateTime> list = query.list();
				assertThat( list.size(), is( 1 ) );
			} );
			checkOneMatch.accept( getOriginalOffsetDateTime() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead().withOffsetSameInstant( ZoneOffset.UTC ) );
		} );
	}

	@Entity(name = ENTITY_NAME)
	static final class EntityWithOffsetDateTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = PROPERTY_COLUMN_NAME)
		private OffsetDateTime value;

		protected EntityWithOffsetDateTime() {
		}

		private EntityWithOffsetDateTime(int id, OffsetDateTime value) {
			this.id = id;
			this.value = value;
		}
	}
}
