/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.hr.service.employee;

import java.math.BigDecimal;

import org.joda.time.LocalDate;

import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.exception.AxelorException;

public interface EmployeeService extends UserService{

//	public BigDecimal getDurationHours(Object object) throws AxelorException;

	public BigDecimal getUserDuration(BigDecimal hourDuration, BigDecimal dailyWorkHrs, boolean toHours);
	
	public int getLengthOfService(Employee employee, LocalDate refDate) throws AxelorException;
	public int getAge(Employee employee, LocalDate refDate) throws AxelorException;

}