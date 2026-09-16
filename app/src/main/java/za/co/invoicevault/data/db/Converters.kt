package za.co.invoicevault.data.db

import androidx.room.TypeConverter
import za.co.invoicevault.domain.DiscountType
import za.co.invoicevault.domain.FolderCategory
import za.co.invoicevault.domain.InvoiceStatus
import za.co.invoicevault.domain.TemplateStyle

class Converters {
    @TypeConverter fun statusToString(value: InvoiceStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): InvoiceStatus = InvoiceStatus.valueOf(value)

    @TypeConverter fun discountToString(value: DiscountType): String = value.name
    @TypeConverter fun stringToDiscount(value: String): DiscountType = DiscountType.valueOf(value)

    @TypeConverter fun styleToString(value: TemplateStyle): String = value.name
    @TypeConverter fun stringToStyle(value: String): TemplateStyle = TemplateStyle.valueOf(value)

    @TypeConverter fun folderToString(value: FolderCategory): String = value.name
    @TypeConverter fun stringToFolder(value: String): FolderCategory = FolderCategory.valueOf(value)
}
