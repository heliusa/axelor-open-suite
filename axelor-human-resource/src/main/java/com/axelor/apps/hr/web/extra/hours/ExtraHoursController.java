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
package com.axelor.apps.hr.web.extra.hours;

import java.util.List;
import java.util.Map;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.base.service.message.MessageServiceBaseImpl;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.ExtraHours;
import com.axelor.apps.hr.db.repo.ExtraHoursRepository;
import com.axelor.apps.hr.service.HRMenuTagService;
import com.axelor.apps.hr.service.extra.hours.ExtraHoursService;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.repo.MessageRepository;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Query;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Provider;


public class ExtraHoursController {

	@Inject
	private Provider<HRMenuTagService> hrMenuTagServiceProvider;
	@Inject
	private Provider<ExtraHoursRepository> extraHoursRepositoryProvider;
	@Inject
	private Provider<ExtraHoursService> extraHoursServiceProvider;
	
	
	public void editExtraHours(ActionRequest request, ActionResponse response){
		List<ExtraHours> extraHoursList = Beans.get(ExtraHoursRepository.class).all().filter("self.user = ?1 AND self.company = ?2 AND self.statusSelect = 1",AuthUtils.getUser(),AuthUtils.getUser().getActiveCompany()).fetch();
		if(extraHoursList.isEmpty()){
			response.setView(ActionView
									.define(I18n.get("Extra Hours"))
									.model(ExtraHours.class.getName())
									.add("form", "extra-hours-form")
									.map());
		}
		else if(extraHoursList.size() == 1){
			response.setView(ActionView
					.define(I18n.get("ExtraHours"))
					.model(ExtraHours.class.getName())
					.add("form", "extra-hours-form")
					.param("forceEdit", "true")
					.context("_showRecord", String.valueOf(extraHoursList.get(0).getId())).map());
		}
		else{
			response.setView(ActionView
					.define(I18n.get("ExtraHours"))
					.model(Wizard.class.getName())
					.add("form", "popup-extra-hours-form")
					.param("forceEdit", "true")
					.param("popup", "true")
					.param("show-toolbar", "false")
					.param("show-confirm", "false")
					.param("forceEdit", "true")
			  		.param("popup-save", "false")
					.map());
		}
	}

	public void validateExtraHours(ActionRequest request, ActionResponse response) throws AxelorException{
		
		User user = AuthUtils.getUser();
		Employee employee = user.getEmployee();
		
		ActionViewBuilder actionView = ActionView.define(I18n.get("Extra hours to Validate"))
				   .model(ExtraHours.class.getName())
				   .add("grid","extra-hours-validate-grid")
				   .add("form","extra-hours-form");
		
		if(employee != null)  {
			actionView.domain("self.company = :activeCompany AND  self.statusSelect = 2")
			.context("activeCompany", user.getActiveCompany());
		
			if(!employee.getHrManager())  {
				if(employee.getManager() != null) {
					actionView.domain(actionView.get().getDomain() + " AND self.user.employee.manager = :user")
					.context("user", user);
				}
				else  {
					actionView.domain(actionView.get().getDomain() + " AND self.user = :user")
					.context("user", user);
				}
			}
		}

		response.setView(actionView.map());
	}
	
	public void editExtraHoursSelected(ActionRequest request, ActionResponse response){
		Map extraHoursMap = (Map)request.getContext().get("extraHoursSelect");
		ExtraHours extraHours = Beans.get(ExtraHoursRepository.class).find(new Long((Integer)extraHoursMap.get("id")));
		response.setView(ActionView
				.define("Extra hours")
				.model(ExtraHours.class.getName())
				.add("form", "extra-hours-form")
				.param("forceEdit", "true")
				.domain("self.id = "+extraHoursMap.get("id"))
				.context("_showRecord", String.valueOf(extraHours.getId())).map());
	}

	public void historicExtraHours(ActionRequest request, ActionResponse response){
		
		User user = AuthUtils.getUser();
		Employee employee = user.getEmployee();
		
		ActionViewBuilder actionView = ActionView.define(I18n.get("Historic colleague extra hours"))
				   .model(ExtraHours.class.getName())
				   .add("grid","extra-hours-grid")
				   .add("form","extra-hours-form");

		if(employee != null && employee.getHrManager())  {
			actionView.domain("self.company = :activeCompany AND (self.statusSelect = 3 OR self.statusSelect = 4)")
			.context("activeCompany", user.getActiveCompany());
		
			if(!employee.getHrManager())  {
				actionView.domain(actionView.get().getDomain() + " AND self.user.employee.manager = :user")
				.context("user", user);
			}
		}
		
		response.setView(actionView.map());
	}
	
	public void showSubordinateExtraHours(ActionRequest request, ActionResponse response)  {
		
		User user = AuthUtils.getUser();
		Company activeCompany = user.getActiveCompany();
		
		ActionViewBuilder actionView = ActionView.define(I18n.get("Extra hours to be Validated by your subordinates"))
		   .model(ExtraHours.class.getName())
		   .add("grid","extra-hours-grid")
		   .add("form","extra-hours-form");
		
		String domain = "self.user.employee.manager.employee.manager = :user AND self.company = :activeCompany AND self.statusSelect = 2";
		
		long nbExtraHours =  Query.of(ExtraHours.class).filter(domain).bind("user", user).bind("activeCompany", activeCompany).count();
		
		if(nbExtraHours == 0)  {
			response.setNotify(I18n.get("No extra hours to be validated by your subordinates"));
		}
		else  {
			response.setView(actionView.domain(domain).context("user", user).context("activeCompany", activeCompany).map());
		}
	}
	
/* Count Tags displayed on the menu items */
	
	public String extraHoursValidateTag() { 
		
		return hrMenuTagServiceProvider.get().countRecordsTag(ExtraHours.class, ExtraHoursRepository.STATUS_CONFIRMED);
	
	}
	
	//confirming request and sending mail to manager
	public void confirm(ActionRequest request, ActionResponse response) throws AxelorException{
		
		try{
			ExtraHours extraHours = request.getContext().asType(ExtraHours.class);
			extraHours = extraHoursRepositoryProvider.get().find(extraHours.getId());
			ExtraHoursService extraHoursService = extraHoursServiceProvider.get();

			extraHoursService.confirm(extraHours);

			Message message = extraHoursService.sendConfirmationEmail(extraHours);
			if(message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT)  {
				response.setFlash(String.format(I18n.get("Email sent to %s"), Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
			} 
			
		}  catch(Exception e)  {
			TraceBackService.trace(response, e);
		}
		finally {
			response.setReload(true);
		}
	}
	
	//validating request and sending mail to applicant
	public void valid(ActionRequest request, ActionResponse response) throws AxelorException{
		
		try{
			ExtraHours extraHours = request.getContext().asType(ExtraHours.class);
			extraHours = extraHoursRepositoryProvider.get().find(extraHours.getId());
			ExtraHoursService extraHoursService = extraHoursServiceProvider.get();
			
			extraHoursService.validate(extraHours);
			
			Message message = extraHoursService.sendValidationEmail(extraHours);
			if(message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT)  {
				response.setFlash(String.format(I18n.get("Email sent to %s"), Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
			} 
			
		}  catch(Exception e)  {
			TraceBackService.trace(response, e);
		}
		finally {
			response.setReload(true);
		}
	}
	
	//refusing request and sending mail to applicant
	public void refuse(ActionRequest request, ActionResponse response) throws AxelorException{
		
		try{
			ExtraHours extraHours = request.getContext().asType(ExtraHours.class);
			extraHours = extraHoursRepositoryProvider.get().find(extraHours.getId());
			ExtraHoursService extraHoursService = extraHoursServiceProvider.get();

			extraHoursService.refuse(extraHours);

			Message message = extraHoursService.sendRefusalEmail(extraHours);
			if(message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT)  {
				response.setFlash(String.format(I18n.get("Email sent to %s"), Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
			} 
			
		}  catch(Exception e)  {
			TraceBackService.trace(response, e);
		}
		finally {
			response.setReload(true);
		}
	}
}
