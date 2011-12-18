/*
 * This file is part of myPlan.
 *
 * Plan is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Plan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with myPlan.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.conzebit.myplan.ext.es.tuenti;

import java.util.ArrayList;

import com.conzebit.myplan.core.Chargeable;
import com.conzebit.myplan.core.call.Call;
import com.conzebit.myplan.core.message.ChargeableMessage;
import com.conzebit.myplan.core.msisdn.MsisdnType;
import com.conzebit.myplan.core.plan.PlanChargeable;
import com.conzebit.myplan.core.plan.PlanSummary;
import com.conzebit.myplan.core.sms.Sms;
import com.conzebit.myplan.ext.es.ESPlan;

/**
 * @author zundr
 */
public abstract class ESTuenti extends ESPlan {

	protected double monthFee;
	protected double initialPrice;
	protected double pricePerSecond;
	protected double smsPrice;
	protected int maxTuSecondsMonth;
	protected int maxTuSmsMonth;
	protected int maxOthersSecondsMonth;
	protected int maxOthersSmsMonth;	
	
	public String getOperator() {
		return MsisdnType.ES_TUENTI.toString();
	}
	
	public PlanSummary process(ArrayList<Chargeable> data) {
		PlanSummary ret = new PlanSummary(this);
		ret.addPlanCall(new PlanChargeable(new ChargeableMessage(ChargeableMessage.MESSAGE_MONTH_FEE), monthFee, this.getCurrency()));

		long tuSecondsTotal = 0;
		long othersSecondsTotal = 0;
		long tuSmsTotal = 0;
		long othersSmsTotal = 0;
		for (Chargeable chargeable : data) {
			if (chargeable.getChargeableType() == Chargeable.CHARGEABLE_TYPE_CALL) {
				Call call = (Call) chargeable;
				if (call.getType() != Call.CALL_TYPE_SENT) {
					continue;
				}
				
				double callPrice = 0;
				
				switch (call.getContact().getMsisdnType()) {
				case ES_SPECIAL_ZER0:
					callPrice = 0;
					break;
					
				case ES_TUENTI:
					tuSecondsTotal += call.getDuration();
					boolean tuDiscount = (tuSecondsTotal <= maxTuSecondsMonth); 
					if (!tuDiscount) {
						boolean callDidPassLimit = (tuSecondsTotal > maxTuSecondsMonth) && (tuSecondsTotal - call.getDuration() <= maxTuSecondsMonth);
						long chargedDuration = callDidPassLimit ? tuSecondsTotal - maxTuSecondsMonth : call.getDuration();  
						callPrice += initialPrice + (chargedDuration * pricePerSecond);
					}
					break;

				default:
					othersSecondsTotal += call.getDuration();
					boolean othersDiscount = (othersSecondsTotal <= maxOthersSecondsMonth); 
					if (!othersDiscount) {
						boolean callDidPassLimit = (othersSecondsTotal > maxOthersSecondsMonth) && (othersSecondsTotal - call.getDuration() <= maxOthersSecondsMonth);
						long chargedDuration = callDidPassLimit ? othersSecondsTotal - maxOthersSecondsMonth : call.getDuration();  
						callPrice += initialPrice + (chargedDuration * pricePerSecond);
					}
					callPrice += initialPrice + (call.getDuration() * pricePerSecond);
					break;
					
				}
				
				ret.addPlanCall(new PlanChargeable(call, callPrice, this.getCurrency()));

			} else if (chargeable.getChargeableType() == Chargeable.CHARGEABLE_TYPE_SMS) {
				Sms sms = (Sms) chargeable;
				if (sms.getType() == Sms.SMS_TYPE_RECEIVED) {
					continue;
				}

				double chargedSmsPrice = 0;
				switch (sms.getContact().getMsisdnType()) {
				case ES_TUENTI:
					tuSmsTotal++;
					if (tuSmsTotal > maxTuSmsMonth) {
						chargedSmsPrice = smsPrice;
					}
						
					break;
					
				default:
					othersSmsTotal++;
					if (othersSmsTotal > maxOthersSmsMonth) {
						chargedSmsPrice = smsPrice;
					}
					break;
				}
				
				ret.addPlanCall(new PlanChargeable(chargeable, chargedSmsPrice, this.getCurrency()));
			}
		}
		return ret;
	}	
}
